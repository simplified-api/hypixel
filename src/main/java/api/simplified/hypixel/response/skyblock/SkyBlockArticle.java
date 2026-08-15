package api.simplified.hypixel.response.skyblock;

import com.google.gson.annotations.SerializedName;
import dev.simplified.annotations.AccessLevel;
import dev.simplified.annotations.Getter;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * One entry of the SkyBlock news feed, as it is shown on the in-game announcements board.
 */
@Getter
public class SkyBlockArticle {

    /**
     * Minecraft material the announcements board draws the entry's icon from.
     */
    @SerializedName("item.material")
    private String material;

    /**
     * Address of the forum thread the entry points at, read through {@link #getUrl()}.
     */
    @Getter(AccessLevel.NONE)
    private String link;

    /**
     * Publication date exactly as the wire spells it, which is display text rather than a timestamp.
     */
    @SerializedName("text")
    private String date;

    /**
     * Headline of the entry.
     */
    private String title;

    /**
     * The forum thread's address parsed into a {@link URL}.
     *
     * @throws IllegalArgumentException if the wire's text is not a valid address
     */
    public URL getUrl() {
        try {
            return new URI(this.link).toURL();
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new IllegalArgumentException(String.format("Unable to create URL '%s'!", this.link));
        }
    }

}
