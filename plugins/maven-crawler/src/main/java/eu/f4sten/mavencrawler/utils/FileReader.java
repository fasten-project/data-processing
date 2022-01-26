/*
 * Copyright 2021 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.mavencrawler.utils;

import static org.codehaus.plexus.PlexusConstants.SCANNING_INDEX;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexUtils;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.updater.IndexDataReader;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.pomanalyzer.data.MavenId;

public class FileReader {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Set<MavenId> readIndexFile(File f) {

        try ( //
                var fis = new FileInputStream(f); //
                var bis = new BufferedInputStream(fis)) {

            var reader = new IndexDataReader(bis);
            IndexingContext context = setupPlexusContext();
            Set<MavenId> artifacts = new HashSet<>();

            reader.readIndex(new IndexDataReader.IndexDataReadVisitor() {
                @Override
                public void visitDocument(Document doc) {
                    MavenId artifact = toMavenId(doc, context);
                    if (artifact == null) {
                        logger.warn("Couldn't construct artifact, skipping.");
                    } else {
                        artifacts.add(artifact);
                    }
                }
            }, context);

            return artifacts;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private IndexingContext setupPlexusContext() {
        PlexusContainer plexusContainer;
        List<IndexCreator> indexers;
        try {
            var pc = new DefaultPlexusContainer();
            var config = new DefaultContainerConfiguration();
            config.setClassWorld(pc.getClassWorld());
            config.setClassPathScanning(SCANNING_INDEX);

            plexusContainer = new DefaultPlexusContainer(config);

            indexers = new ArrayList<IndexCreator>();
            for (Object component : plexusContainer.lookupList(IndexCreator.class)) {
                indexers.add((IndexCreator) component);
            }

        } catch (PlexusContainerException e) {
            throw new RuntimeException("Cannot construct PlexusContainer for MavenCrawler.", e);
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Cannot add IndexCreators for MavenCrawler.", e);
        }

        var context = (IndexingContext) Proxy.newProxyInstance( //
                getClass().getClassLoader(), //
                new Class[] { IndexingContext.class }, //
                new MyInvocationHandler(indexers));
        return context;
    }

    public static MavenId toMavenId(Document document, IndexingContext context) {
        ArtifactInfo ai = IndexUtils.constructArtifactInfo(document, context);
        if (ai == null) {
            return null;
        }

        var id = new MavenId();
        id.artifactId = ai.getArtifactId();
        id.groupId = ai.getGroupId();
        id.version = ai.getVersion();
        return id;
    }

    public static class MyInvocationHandler implements InvocationHandler {

        private List<IndexCreator> indexers;

        public MyInvocationHandler(List<IndexCreator> indexers) {
            this.indexers = indexers;
        }

        public List<IndexCreator> getIndexCreators() {
            return indexers;
        }

        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            try {
                final Method localMethod = getClass().getMethod(method.getName(), method.getParameterTypes());
                return localMethod.invoke(this, args);
            } catch (NoSuchMethodException e) {
                throw new UnsupportedOperationException("Method " + method.getName() + "() is not supported");
            } catch (IllegalAccessException e) {
                throw new UnsupportedOperationException("Method " + method.getName() + "() is not supported");
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}