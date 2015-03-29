package com.bikelock.bikelock.model;

/**
 * Created by alec on 3/2/15.
 */
public class PairedDevice {
    private String address, name, password;
    private boolean isPrimary;



    public PairedDevice(String address, String name, String password, boolean isPrimary) {
        this.address = address;
        this.name = name;
        this.password = password;
        this.isPrimary = isPrimary;

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PairedDevice that = (PairedDevice) o;

        if (!address.equals(that.address)) return false;
        if (!name.equals(that.name)) return false;
        if (!password.equals(that.password)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = address.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + password.hashCode();
        return result;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
