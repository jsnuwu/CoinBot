package dev.coinbot.mobile;

import android.text.Html;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.regex.*;

final class XReader {
    static final class SourceError extends Exception {
        SourceError(String message) { super(message); }
    }
    static String text(Core.Candidate candidate, java.util.Set<String> allowed) throws Exception {
        String address = "https://publish.x.com/oembed?omit_script=true&dnt=true&url=" + URLEncoder.encode(candidate.url, "UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(8_000); connection.setReadTimeout(8_000);
        connection.setRequestProperty("User-Agent", "CoinBot-Android/0.1");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new SourceError("X-Abfrage abgelehnt (HTTP " + status + ")");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    if (out.size() > 100_000) throw new SourceError("X-Antwort zu groß");
                }
            }
            JSONObject payload = new JSONObject(out.toString("UTF-8"));
            String author = null;
            for (String h : allowed) if (Core.authorMatches(payload.optString("author_url"), h)) author = h;
            if (author == null || (!candidate.handle.isEmpty() && !candidate.handle.equals(author))) throw new SourceError("X-Absender nicht bestätigt");
            Core.Candidate confirmed = Core.candidate(payload.optString("url"), java.util.Collections.singleton(author));
            if (confirmed == null || !confirmed.id.equals(candidate.id)) throw new SourceError("X-Post nicht bestätigt");
            Matcher paragraph = Pattern.compile("<p(?:\\s[^>]*)?>(.*?)</p>", Pattern.DOTALL).matcher(payload.optString("html"));
            if (!paragraph.find()) throw new SourceError("Kein lesbarer Post-Text");
            String body = paragraph.group(1).replaceAll("(?is)<a\\b[^>]*>.*?</a>", " ");
            String value = Html.fromHtml(body, Html.FROM_HTML_MODE_LEGACY).toString().trim();
            String withoutUrls=value.replaceAll("https?://\\S+", "").trim();
            if (value.isEmpty() || withoutUrls.endsWith("…") || withoutUrls.endsWith("...")) throw new SourceError("X liefert keinen vollständigen Post-Text");
            return value;
        } finally { connection.disconnect(); }
    }
}
