package com.savonia.thesis.webclient.measuremetsmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DataModel {

    @SerializedName("BinaryValue")
    @Expose
    private byte[] binaryValue;
    @SerializedName("BinaryValueBase64")
    @Expose
    private String binaryValueBase64;
    @SerializedName("LongValue")
    @Expose
    private long longValue;
    @SerializedName("Tag")
    @Expose
    private String tag;
    @SerializedName("TextValue")
    @Expose
    private String textValue;
    @SerializedName("Value")
    @Expose
    private double value;
    @SerializedName("XmlValue")
    @Expose
    private String xmlValue;

    public DataModel(){}

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    public void setBinaryValue(byte[] binaryValue) {
        this.binaryValue = binaryValue;
    }

    public String getBinaryValueBase64() {
        return binaryValueBase64;
    }

    public void setBinaryValueBase64(String binaryValueBase64) {
        this.binaryValueBase64 = binaryValueBase64;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getXmlValue() {
        return xmlValue;
    }

    public void setXmlValue(String xmlValue) {
        this.xmlValue = xmlValue;
    }

}
