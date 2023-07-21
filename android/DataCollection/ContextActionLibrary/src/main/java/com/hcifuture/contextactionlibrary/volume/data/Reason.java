package com.hcifuture.contextactionlibrary.volume.data;

import java.util.Objects;

public class Reason {
    String id;
    String name;

    public Reason(String id, String reason) {
        this.id = id;
        this.name = reason;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reason reason1 = (Reason) o;
        return Objects.equals(name, reason1.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
