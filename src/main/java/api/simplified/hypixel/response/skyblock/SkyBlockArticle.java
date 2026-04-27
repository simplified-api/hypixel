package api.simplified.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Getter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Getter
public class SkyBlockArticle {

    @SerializedName("item.material")
    private String material;
    @Getter(AccessLevel.NONE)
    private String link;
    @SerializedName("text")
    private String date;
    private String title;

    public URL getUrl() {
        try {
            return new URI(this.link).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new IllegalArgumentException(String.format("Unable to create URL '%s'!", this.link));
        }
    }

}
