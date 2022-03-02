package eu.f4sten.vulchainfinder.utils;

import eu.fasten.core.data.callableindex.RocksDao;

public class CallableIndexUtils {
    private final RocksDao dao;

    public CallableIndexUtils(RocksDao context) {
        this.dao = context;
    }

    public RocksDao getDao() {
        return dao;
    }
}
