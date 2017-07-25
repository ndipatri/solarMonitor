package com.ndipatri.solarmonitor.providers.panelScan;


import java.util.Optional;

public class PanelInfo {

    private String description;
    private Optional<String> customerId = Optional.empty();

    public PanelInfo() {
        this.description = "new panel";
    }

    public PanelInfo(final String description, final String customerId) {
        this.description = description;
        this.customerId = Optional.ofNullable(customerId);
    }

    public PanelInfo(String[] attachment) {
        this.description = attachment[0];
        this.customerId = Optional.ofNullable(attachment[1]);
    }

    public String[] getAttachment() {
        return new String[]{description, (customerId.isPresent() ? customerId.get() : null)};
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<String> getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = Optional.ofNullable(customerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PanelInfo panelInfo = (PanelInfo) o;

        if (description != null ? !description.equals(panelInfo.description) : panelInfo.description != null)
            return false;
        return customerId != null ? customerId.equals(panelInfo.customerId) : panelInfo.customerId == null;

    }

    @Override
    public int hashCode() {
        int result = description != null ? description.hashCode() : 0;
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PanelInfo{" +
                "description='" + description + '\'' +
                ", customerId='" + customerId + '\'' +
                '}';
    }
}
