package mes.app.pda.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/pda/apk")
public class ApkController {

    private static final String APK_DIR = "C:/temp/apk/bogo/";
    private static final String VERSION_FILE = "C:/temp/apk/bogo/version.json";

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadApk(@PathVariable String fileName) throws IOException {
        Path path = Paths.get(APK_DIR + fileName);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(path.toUri());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .body(resource);
    }

    @GetMapping("/version")
    public ResponseEntity<String> getVersionInfo() throws IOException {
        Path path = Paths.get(VERSION_FILE);
        if (!Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }

        String json = Files.readString(path);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }

}
