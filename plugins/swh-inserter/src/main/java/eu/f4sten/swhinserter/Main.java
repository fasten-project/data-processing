package eu.f4sten.swhinserter;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.Plugin;
import eu.f4sten.infra.kafka.Kafka;
import eu.f4sten.infra.kafka.Lane;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.pomanalyzer.utils.DatabaseUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

public class Main implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final SwhInserterArgs args;
    private final Kafka kafka;
    private final DatabaseUtils db;
    private final IoUtils io;

    @Inject
    public Main(SwhInserterArgs args, Kafka kafka, DatabaseUtils db, IoUtils io) {
        this.args = args;
        this.kafka = kafka;
        this.db = db;
        this.io = io;
    }

    @Override
    public void run() {

        try {
            AssertArgs.assertFor(args)//
                    .notNull(a -> a.kafkaIn, "kafka input topic"); //

            LOG.info("Subscribing to '{}'", args.kafkaIn);
            kafka.subscribe(args.kafkaIn, LinkedHashMap.class, this::consume);
            while (true) {
                LOG.debug("Polling ...");
                kafka.poll();
            }
        } finally {
            kafka.stop();
        }
    }

    private void consume(LinkedHashMap<String, String> message, Lane lane) {
        var json = new JSONObject(message);
        LOG.info("Consuming next {} record {} ...", lane, json);
        var pkgName = json.get("product").toString();
        var ver = json.get("version").toString();
        //var srcPath = json.get("version").toString();

        var pkgVerID = db.getPkgVersionID(pkgName, ver);
        var pkgVerFilesPaths = db.getFilePaths4PkgVersion(pkgVerID);

        pkgVerFilesPaths.forEach(fp -> {
            LOG.info("P: {}", fp);
            var srcFileContent = readSrcFileContent(pkgName, ver, fp);
            var srcFileHash = computeGitHash(srcFileContent.getBytes(StandardCharsets.UTF_8));
            db.addFileHash(pkgVerID, fp, srcFileHash);
            LOG.info("Added file hash for {}", fp);
        });
    }

    private String readSrcFileContent(String pkgName, String version, String filePath) {
        String[] ga = pkgName.split(":");
        var groupID = ga[0];
        var artifactID = ga[1];
        var baseDir = io.getBaseFolder();
        var srcFile = new File(Path.of(baseDir.toString(), "sources", "mvn", Character.toString(groupID.charAt(0)),
                groupID, artifactID, version, filePath).toString());
        try {
            return FileUtils.readFileToString(srcFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read the file " + srcFile.toPath());
        }
    }

    // This method computes a SWH-compatible hash
    private String computeGitHash(byte[] fileContent) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        md.update(String.format("blob %d\u0000", fileContent.length).getBytes());
        md.update(fileContent);
        return Hex.encodeHexString(md.digest());
    }
}
