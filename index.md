---
layout: default
---

#About

JRAW is designed to be an incredibly customizable API wrapper for Reddit written in Java. It supports most every common action one can do on the site, as well as being the only API wrapper that supports OAuth2. As an example, this code will retrieve the first page from the front page:

```java
RedditClient reddit = new RedditClient(...)
for (Submission link : new SubredditPaginator(reddit).next()) {
	System.out.printf("%s↑ /r/%s - %s\n", link.getScore(), link.getSubreddit(), link.getTitle())
}
```

This results in:

```
4069↑ /r/science - Randomized double-blind placebo-controlled trial shows non-celiac gluten sensitivity is indeed real
3541↑ /r/pics - You would think it it would be gold at the end of our irish rainbows.
1883↑ /r/todayilearned - TIL that Wisconsin's only pirate once got the crew of an entire ship drunk then proceeded to throw them all over board. He then took the ship to Chicago and sold all the cargo.
...
```

#Getting Started

The [Quickstart wiki page](https://github.com/thatJavaNerd/JRAW/wiki/Quickstart) can help you get started.

#Javadoc

{% include javadoc_table.md %}
