package com.mindmap.view.settings;

import com.mindmap.config.UserPreferences;
import com.mindmap.util.AppPreferences;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class AppearancePanel extends VBox {
    public AppearancePanel(UserPreferences prefs) {
        setSpacing(12);
        
        Label header = new Label("🎨  Appearance");
        header.getStyleClass().add("section-header");

        Label desc = new Label("Customize your experience. (Theme changes require restart)");
        desc.getStyleClass().add("placeholder-desc");
        desc.setWrapText(true);

        ToggleButton themeBtn = new ToggleButton(AppPreferences.isDarkMode() ? "Dark Mode" : "Light Mode");
        themeBtn.setOnAction(e -> {
            AppPreferences.setDarkMode(!AppPreferences.isDarkMode());
            themeBtn.setText(AppPreferences.isDarkMode() ? "Dark Mode" : "Light Mode");
        });

        ColorPicker colorPicker = new ColorPicker(Color.web(AppPreferences.getAccentColor()));
        colorPicker.setOnAction(e -> {
            String hex = String.format("#%02x%02x%02x",
                    (int) (colorPicker.getValue().getRed() * 255),
                    (int) (colorPicker.getValue().getGreen() * 255),
                    (int) (colorPicker.getValue().getBlue() * 255));
            AppPreferences.setAccentColor(hex);
        });

        HBox row = new HBox(12, new Label("Theme:"), themeBtn, new Label("Accent Color:"), colorPicker);
        row.setAlignment(Pos.CENTER_LEFT);
        
        getChildren().addAll(header, desc, row);
    }
}
