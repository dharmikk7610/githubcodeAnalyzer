package com.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import com.DTO.GithubUserDTO;
import com.Service.TextsplitterService;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

@RestController
public class Reviewcontroller {

    @Autowired
    private RestClient restClient;

    @Autowired
    private TextsplitterService tsService;

    // ─────────────────────────────────────────────
    // These are reset at the start of every request
    // ─────────────────────────────────────────────
    private final StringBuilder allCode   = new StringBuilder();
    private final StringBuilder aiReview  = new StringBuilder();


    // ══════════════════════════════════════════════
    // POST /reviewrepo
    // Body: { "userrepo": "https://github.com/user/repo", "folder": "src/main/java" }
    // ══════════════════════════════════════════════
    @PostMapping("/reviewrepo")
    public ResponseEntity<?> reviewRepo(@RequestBody GithubUserDTO gdto) {

        // Reset state for each new request
        allCode.setLength(0);
        aiReview.setLength(0);

        // ── 1. Validate & normalize GitHub URL ──
        String gitUrl = gdto.getUserrepo();
        String folder = gdto.getFolder();

        if (gitUrl == null || gitUrl.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Github url is required");
        }

        if (!gitUrl.startsWith("https://")) {
            gitUrl = "https://" + gitUrl;
        }

        if (gitUrl.endsWith("/")) {
            gitUrl = gitUrl.substring(0, gitUrl.length() - 1);
        }

        // ── 2. Parse owner and repo from URL ──
        String[] parts = gitUrl.split("/");

        if (parts.length < 5) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Github url is invalid. Expected: https://github.com/owner/repo");
        }

        String owner = parts[3];
        String repo  = parts[4];

        // ── 3. Treat null/blank folder as repo root ──
        if (folder == null || folder.isBlank()) {
            folder = "";
        }

        System.out.println("Owner  : " + owner);
        System.out.println("Repo   : " + repo);
        System.out.println("Folder : " + (folder.isBlank() ? "(root)" : folder));

        // ── 4. Scan and collect all Java files ──
        try {

            scanFolder(owner, repo, folder);

            if (allCode.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body("No Java files found in the specified path.");
            }

            System.out.println("Total code length : " + allCode.length());

            // ── 5. Split into chunks ──
            Document document = Document.from(allCode.toString());
            List<TextSegment> chunks = tsService.splitData(document);

            System.out.println("Total chunks : " + chunks.size());

            // ── 6. Review each chunk ──
            for (int i = 0; i < chunks.size(); i++) {

                TextSegment chunk = chunks.get(i);

                System.out.println("Chunk [" + (i + 1) + "/" + chunks.size() + "] "
                        + "size = " + chunk.text().length() + " chars");

                System.out.println("Sending chunk " + (i + 1) + " to AI...");

                String result = tsService.aiReview(chunk.text());

                System.out.println("Chunk " + (i + 1) + " reviewed. "
                        + "Response length = " + result.length());

                aiReview
                        .append("── Chunk ")
                        .append(i + 1)
                        .append(" of ")
                        .append(chunks.size())
                        .append(" ──\n")
                        .append(result)
                        .append("\n\n====================\n\n");
            }
            
            String last_response = tsService.finalRepositoryReview(aiReview.toString());

            // ── 7. Return final review ──
            Map<String, Object> response = new HashMap<>();
            response.put("owner",  owner);
            response.put("repo",   repo);
            response.put("folder", folder.isBlank() ? "(root)" : folder);
            response.put("totalChunks", chunks.size());
            response.put("review", last_response);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error : " + e.getMessage());
        }
    }


    // ══════════════════════════════════════════════
    // Recursively scan a GitHub folder via API
    // Collects all .java file contents into allCode
    // ══════════════════════════════════════════════
    private void scanFolder(String owner, String repo, String folder) {

        String apiUrl = "https://api.github.com/repos/"
                + owner + "/" + repo + "/contents/" + folder;

        System.out.println("Scanning : " + apiUrl);

        Object response;

        try {
            response = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(Object.class);

        } catch (Exception e) {
            System.err.println("Failed to fetch : " + apiUrl + " → " + e.getMessage());
            return;
        }

        // ── List response = folder ──
        if (response instanceof List<?> list) {

            for (Object item : list) {

                if (!(item instanceof Map<?, ?> map)) continue;

                String type        = (String) map.get("type");
                String path        = (String) map.get("path");
                String name        = (String) map.get("name");
                String downloadUrl = (String) map.get("download_url");

                if ("dir".equals(type)) {
                    // Recurse into subdirectory
                    scanFolder(owner, repo, path);

                } else if (name != null && name.endsWith(".java")) {
                    fetchAndAppend(path, downloadUrl);
                }
            }
        }

        // ── Map response = single file ──
        else if (response instanceof Map<?, ?> map) {

            String type        = (String) map.get("type");
            String path        = (String) map.get("path");
            String name        = (String) map.get("name");
            String downloadUrl = (String) map.get("download_url");

            if ("file".equals(type) && name != null && name.endsWith(".java")) {
                fetchAndAppend(path, downloadUrl);
            }
        }
    }


    // ══════════════════════════════════════════════
    // Download a single file and append to allCode
    // ══════════════════════════════════════════════
    private void fetchAndAppend(String path, String downloadUrl) {

        if (downloadUrl == null || downloadUrl.isBlank()) {
            System.err.println("No download URL for : " + path);
            return;
        }

        System.out.println("Fetching : " + path);

        try {
            String code = restClient.get()
                    .uri(downloadUrl)
                    .retrieve()
                    .body(String.class);

            allCode
                    .append("\n// ── FILE: ")
                    .append(path)
                    .append(" ──\n")
                    .append(code)
                    .append("\n");

            System.out.println("Fetched  : " + path + " (" + code.length() + " chars)");

        } catch (Exception e) {
            System.err.println("Failed to fetch file : " + path + " → " + e.getMessage());
        }
    }
}