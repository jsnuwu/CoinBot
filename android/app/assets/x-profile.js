(function () {
  'use strict';
  // Only public post DOM. Never read input values, cookies, storage or request tokens.
  const page = location.href;
  const signedIn = !!document.querySelector('[data-testid="SideNav_AccountSwitcher_Button"], [data-testid="AppTabBar_Notifications_Link"], a[href="/notifications"]');
  const gated = !!document.querySelector('input[type="password"], input[autocomplete="username"]');
  const posts = [];
  if (signedIn && !gated) {
    for (const article of Array.from(document.querySelectorAll('article[data-testid="tweet"]')).slice(0, 20)) {
      const times = Array.from(article.querySelectorAll('a[href*="/status/"] time'));
      const time = times[0];
      const anchor = time && time.closest('a');
      const text = article.querySelector('[data-testid="tweetText"]');
      if (!anchor || !text || !time) continue;
      const href = new URL(anchor.getAttribute('href'), page).href;
      const quoted = times.some(t => t.closest('a').getAttribute('href') !== anchor.getAttribute('href')) ||
        !!article.querySelector('[data-testid="quoteTweet"], div[role="link"] a[href*="/status/"]');
      posts.push({url: href, text: (text.innerText || text.textContent || '').slice(0, 4001),
        createdAt: time.getAttribute('datetime') || '',
        context: quoted || !!article.querySelector('[data-testid="socialContext"], [data-testid="promotedIndicator"], [data-testid="replyingTo"], a[href*="/history"]'),
        truncated: !!article.querySelector('[data-testid="tweet-text-show-more-link"]')});
    }
  }
  return JSON.stringify({page, signedIn, gated, posts});
})();
