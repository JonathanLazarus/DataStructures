package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;

import java.net.URI;
import java.util.HashMap;

class UriTimeVault implements Comparable<UriTimeVault> {
    private URI uriKey;
    private long lastUsedTime;
    private int byteCount;

    UriTimeVault(URI uri, long time, int byteCount) {
        this.uriKey = uri;
        this.lastUsedTime = time;
        this.byteCount = byteCount;
    }

    protected int getByteCount() {
        return this.byteCount;
    }

    protected long getLastUsedTime() {
        return lastUsedTime;
    }

    protected void setLastUsedTime(long lastUsedTime) {
        this.lastUsedTime = lastUsedTime;
    }


    protected URI getKey() {
        return uriKey;
    }

    protected void setUri(URI uri) {
        this.uriKey = uri;
    }

    @Override
    public int hashCode() {
        return this.uriKey.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (obj.getClass() != this.getClass()) return false;
        UriTimeVault timeObj = (UriTimeVault) obj;
        if (this.uriKey.equals(timeObj.uriKey)) {
            return this.lastUsedTime == timeObj.lastUsedTime;
        }
        return false;
    }

    @Override
    public int compareTo(UriTimeVault o) {
        return Long.compare(this.lastUsedTime, o.getLastUsedTime());
    }
}
