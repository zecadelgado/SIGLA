package br.com.sigla.desktop.navigation;

public enum AppView {
    INVENTORY("/fxml/inventory/inventory-view.fxml"),
    CUSTOMER("/fxml/customer/customer-view.fxml"),
    BILLING("/fxml/billing/billing-view.fxml"),
    NOTIFICATION("/fxml/notification/notification-view.fxml"),
    FISCAL("/fxml/fiscal/fiscal-view.fxml");

    private final String fxmlPath;

    AppView(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String fxmlPath() {
        return fxmlPath;
    }
}
