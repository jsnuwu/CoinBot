(function () {
  'use strict';
  // Only public post DOM. Never read input values, cookies, storage or request tokens.
  const page = location.href;
  const signedIn = !!document.querySelector('[data-testid="SideNav_AccountSwitcher_Button"], [data-testid="AppTabBar_Notifications_Link"], a[href="/notifications"]');
  const gated = !!document.querySelector('input[type="password"], input[autocomplete="username"]');
  const posts = [];
  if (signedIn && !gated) {
    for (const article of Array.from(document.querySelectorAll('article[data-testid="tweet"]')).slice(0, 20)) {
      if (article.parentElement.closest('article[data-testid="tweet"]')) continue;
      // Quote cards have their own text/time. Never attach those to the outer post's permalink.
      const outer = node => {
        for (let parent=node.parentElement; parent && parent!==article; parent=parent.parentElement) {
          if (parent.matches('[data-testid="quoteTweet"], div[role="link"], article')) return false;
        }
        return true;
      };
      const times = Array.from(article.querySelectorAll('a[href*="/status/"] time')).filter(outer);
      const time = times[0];
      const anchor = time && time.closest('a');
      const text = Array.from(article.querySelectorAll('[data-testid="tweetText"]')).find(outer);
      if (!anchor || !text || !time) continue;
      const href = new URL(anchor.getAttribute('href'), page).href;
      const copy = text.cloneNode(true);
      for (const image of copy.querySelectorAll('img')) image.replaceWith(image.getAttribute('alt') || '');
      for (const br of copy.querySelectorAll('br')) br.replaceWith('\n');
      const body = copy.innerText || copy.textContent || '';
      posts.push({url: href, text: body.slice(0, 4001),
        createdAt: time.getAttribute('datetime') || '',
        context: Array.from(article.querySelectorAll('[data-testid="socialContext"], [data-testid="promotedIndicator"], [data-testid="replyingTo"], a[href*="/history"]')).some(outer),
        truncated: Array.from(article.querySelectorAll('[data-testid="tweet-text-show-more-link"]')).some(outer)});
    }
  }
  return JSON.stringify({page, signedIn, gated, posts});
})();
