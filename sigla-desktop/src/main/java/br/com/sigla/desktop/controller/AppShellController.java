package br.com.sigla.desktop.controller;

import br.com.sigla.desktop.navigation.AppView;
import br.com.sigla.desktop.navigation.NavigationManager;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import org.springframework.stereotype.Component;

@Component
public class AppShellController {

    @FXML
    private StackPane contentHost;
    private NavigationManager navigationManager;

    public void bindNavigation(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;
        this.navigationManager.bindHost(contentHost);
        this.navigationManager.navigateTo(AppView.INVENTORY);
    }

    @FXML
    private void onInventoryClick() {
        navigate(AppView.INVENTORY);
    }

    @FXML
    private void onCustomerClick() {
        navigate(AppView.CUSTOMER);
    }

    @FXML
    private void onBillingClick() {
        navigate(AppView.BILLING);
    }

    @FXML
    private void onNotificationClick() {
        navigate(AppView.NOTIFICATION);
    }

    @FXML
    private void onFiscalClick() {
        navigate(AppView.FISCAL);
    }

    private void navigate(AppView view) {
        if (navigationManager != null) {
            navigationManager.navigateTo(view);
        }
    }
}
