package com.example.bottomnavigationapp.service;

public class ApiResponse {
    private Result[] results;

    public Result[] getResults() {
        return results;
    }

    public void setResults(Result[] results) {
        this.results = results;
    }

    public static class Result {
        private String trackName;
        private String artistName;
        private String previewUrl;
        private String trackId;

        public String getTrackName() {
            return trackName;
        }

        public void setTrackName(String trackName) {
            this.trackName = trackName;
        }

        public String getArtistName() {
            return artistName;
        }

        public void setArtistName(String artistName) {
            this.artistName = artistName;
        }

        public String getPreviewUrl() {
            return previewUrl;
        }

        public void setPreviewUrl(String previewUrl) {
            this.previewUrl = previewUrl;
        }

        public String getTrackId() {
            return trackId;
        }

        public void setTrackId(String trackId) {
            this.trackId = trackId;
        }
    }
}
