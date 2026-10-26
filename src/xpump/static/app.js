const el = id => document.getElementById(id);
let current = null;
let refreshTimer;
const date = value => value ? new Date(value).toLocaleString('de-DE') : '–';
function paragraph(text, className = '') {
  const p = document.createElement('p'); p.textContent = text; p.className = className; return p;
}
function showResult(data) {
  current = data;
  el('meme').src = 'data:image/png;base64,' + data.image;
  el('download-image').href = el('meme').src;
  el('coin-name').textContent = `${data.metadata.name} · $${data.metadata.symbol}`;
  el('coin-description').textContent = data.metadata.description;
  el('origin').textContent = data.origin === 'user_supplied_text'
    ? 'Vorschlag aus eingegebenem Text. Der Text wurde nicht mit X abgeglichen. Kein Token veröffentlicht.'
    : 'Vorschlag aus der experimentellen öffentlichen X-Ansicht. Kein Token veröffentlicht.';
  el('result').hidden = false;
}
async function loadStatus() {
  clearTimeout(refreshTimer);
  try {
    const response = await fetch('/api/status');
    if (!response.ok) throw new Error('Status nicht erreichbar');
    const data = await response.json(), monitor = data.monitor;
    const labels = {starting:'Prüfung startet',polling:'Öffentliche Ansicht',degraded:'Quelle gestört',not_configured:'Kein Account gesetzt'};
    el('feed-status').textContent = labels[monitor.status] || 'Unbekannter Status';
    el('feed-detail').textContent = `Experimentelle Abfrage alle ${data.poll_seconds} Sekunden, solange der Dienst läuft. ${monitor.checks} Prüfungen seit Start.`;
    el('last-check').textContent = date(monitor.last_checked);
    el('started-at').textContent = date(monitor.started_at);
    el('latest-post').textContent = date(monitor.latest_post_at);
    el('feed-error').textContent = monitor.status === 'degraded'
      ? 'Mindestens ein Account ist nicht verfügbar. Erreichbare Accounts werden weiter geprüft. Keine bezahlte Ersatz-API; ältere Posts können veraltet sein.' : '';
    el('accounts').replaceChildren();
    const accountLabels = {readable:'Daten geliefert',no_posts_returned:'Keine Posts geliefert',unavailable:'X-Zugriff derzeit nicht verfügbar',processing_error:'Verarbeitung fehlgeschlagen',starting:'Prüfung startet'};
    const errorLabels = {public_access_limited:'X schränkt den öffentlichen Zugriff ein',public_view_unavailable:'Öffentliche Ansicht nicht lesbar',public_view_too_large:'Antwort zu groß',request_timeout:'Zeitüberschreitung',network_error:'Netzwerkfehler',invalid_response_encoding:'Unlesbare Antwort',reader_error:'Lesefehler',pipeline_error:'Vorschlagsverarbeitung fehlgeschlagen'};
    for (const account of monitor.accounts || []) {
      const reason = account.error_code ? ` ${errorLabels[account.error_code] || 'Quelle nicht lesbar'}${account.http_status ? ` (HTTP ${account.http_status})` : ''}.` : '';
      el('accounts').append(paragraph(`@${account.handle}: ${accountLabels[account.status] || account.status}.${reason} Nächste Prüfung: ${date(account.next_check_at)}`, 'muted small'));
    }
    el('posts').replaceChildren();
    for (const post of (monitor.recent_posts || []).slice(0, 5)) {
      const article = document.createElement('article');
      const link = document.createElement('a'); link.href = post.url; link.target = '_blank'; link.rel = 'noopener noreferrer';
      link.textContent = `@${post.handle} · ${date(post.created_at)}`;
      const button = document.createElement('button'); button.className = 'secondary'; button.textContent = 'Text übernehmen';
      button.onclick = () => {el('post-text').value = post.text; el('post-url').value = post.url; el('post-text').focus();};
      article.append(link, paragraph(post.text), button); el('posts').append(article);
    }
    if (!monitor.recent_posts?.length) el('posts').append(paragraph('Noch keine Posts geliefert. Der Status oben zeigt, ob die Abfrage läuft.', 'muted'));
    el('previews').replaceChildren();
    const prepared = data.previews.filter(p => p.status === 'preview');
    for (const preview of prepared) {
      const idea = JSON.parse(preview.idea), article = document.createElement('article');
      const button = document.createElement('button'); button.className = 'secondary'; button.textContent = 'Vorschlag ansehen';
      button.onclick = async () => {
        try {
          const response = await fetch('/api/previews/' + encodeURIComponent(preview.root_id));
          if (!response.ok) throw new Error();
          showResult(await response.json()); el('result').scrollIntoView({behavior:'auto',block:'center'});
        } catch {el('form-status').textContent = 'Vorschlag nicht mehr verfügbar. Der Serverspeicher ist temporär.';}
      };
      article.append(paragraph(`${idea.name} · $${idea.symbol}`), button); el('previews').append(article);
    }
    if (!prepared.length) el('previews').append(paragraph('Noch kein neuer Post hat die Auswahlregeln erfüllt. Keine erfundenen Launches oder Gewinne.', 'muted'));
  } catch {
    el('feed-status').textContent = 'Nicht erreichbar';
    el('feed-error').textContent = 'Der Dienst startet möglicherweise gerade oder schläft. Die letzte Anzeige ist nicht aktuell.';
  } finally {
    // Only refresh a visible page. No keep-alive pings for sleeping Render services.
    if (!document.hidden) refreshTimer = setTimeout(loadStatus, 15000);
  }
}
el('preview-form').addEventListener('submit', async event => {
  event.preventDefault(); el('generate').disabled = true; el('form-status').textContent = 'Erstelle lokalen Vorschlag …';
  el('result').hidden = true; current = null;
  try {
    const response = await fetch('/api/preview', {method:'POST',headers:{'Content-Type':'application/json'},
      body:JSON.stringify({text:el('post-text').value.trim(),url:el('post-url').value.trim()})});
    const data = await response.json();
    if (!response.ok) throw new Error(data.error || 'Vorschlag konnte nicht erstellt werden.');
    showResult(data); el('form-status').textContent = 'Vorschlag fertig. Keine API-Gebühr, kein Blockchain-Launch.';
  } catch (error) {el('form-status').textContent = error.message || 'Verbindung fehlgeschlagen.';}
  finally {el('generate').disabled = false;}
});
el('demo').onclick = () => {el('post-text').value = 'Coffee on the moon!';el('post-url').value = '';el('post-text').focus();};
el('download-json').onclick = () => {
  if (!current) return;
  const url = URL.createObjectURL(new Blob([JSON.stringify(current.metadata,null,2)], {type:'application/json'}));
  const link = document.createElement('a'); link.href = url; link.download = 'metadata.json'; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
};
document.addEventListener('visibilitychange', () => {clearTimeout(refreshTimer);if (!document.hidden) loadStatus();});
loadStatus();
