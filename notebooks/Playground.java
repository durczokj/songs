import java.nio.file.Files;
import java.nio.file.Path;

import com.songs.http.JdkHttpClient;
import com.songs.model.Playlist;
import com.songs.model.SyncPlan;
import com.songs.model.SyncResult;
import com.songs.provider.youtube.YouTubeAudioProvider;
import com.songs.provider.youtube.YtDlpConfig;
import com.songs.repository.apple.ApplePlaylistRepository;
import com.songs.repository.local.LocalPlaylistRepository;
import com.songs.repository.local.TagReader;
import com.songs.sync.PlaylistSynchronizer;

public class Playground {

    public static void main(String[] args) throws Exception {
        String sourceUri = "https://music.apple.com/pl/playlist/favourite-107950/pl.u-xlyNqGYue3GNz0";
        String targetUri = "file:///Users/jakubdurczok/Documents/GitHub/songs/data/full/";

        ApplePlaylistRepository apple = new ApplePlaylistRepository();
        LocalPlaylistRepository local = new LocalPlaylistRepository(
            new YouTubeAudioProvider(new JdkHttpClient(), new YtDlpConfig()),
            new TagReader()
        );

        Playlist source = apple.extract(sourceUri);
        System.out.println(source);
        Playlist target = local.extract(targetUri);
        System.out.println(target);

        // 3. Plan the sync (pure — no I/O).
        PlaylistSynchronizer sync = new PlaylistSynchronizer();
        SyncPlan plan = sync.plan(source, target);
        System.out.println();
        System.out.println(plan);

        // 4. Apply the plan (effectful — downloads + tags + deletes).
        System.out.println("Applying plan...");
        SyncResult result = sync.apply(plan, local);
        System.out.println(result);
    }
}
