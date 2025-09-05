package org.to.telegramfinalproject.UI;

import javafx.beans.binding.Bindings;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;

public final class AvatarFX {

    public static void circleClip(ImageView iv, double sizePx) {
        iv.setFitWidth(sizePx);
        iv.setFitHeight(sizePx);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        Circle c = new Circle();
        c.radiusProperty().bind(Bindings.min(iv.fitWidthProperty(), iv.fitHeightProperty()).divide(2));
        c.centerXProperty().bind(iv.fitWidthProperty().divide(2));
        c.centerYProperty().bind(iv.fitHeightProperty().divide(2));
        iv.setClip(c);
    }
}
