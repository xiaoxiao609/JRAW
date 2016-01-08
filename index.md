---
layout: default
---

<!--- Fork me on GitHub -->
<a href="https://github.com/thatJavaNerd/JRAW"><img style="position: absolute; top: 0; right: 0; border: 0;" src="https://camo.githubusercontent.com/a6677b08c955af8400f44c6298f40e7d19cc5b2d/68747470733a2f2f73332e616d617a6f6e6177732e636f6d2f6769746875622f726962626f6e732f666f726b6d655f72696768745f677261795f3664366436642e706e67" alt="Fork me on GitHub" data-canonical-src="https://s3.amazonaws.com/github/ribbons/forkme_right_gray_6d6d6d.png"></a>

#About

JRAW is an easy-to-understand API wrapper for reddit written in Java. It supports OAuth2 authentication as well as every common user action. Using the library is simple:

```java
RedditClient reddit = new RedditClient(...)
reddit.authenticate(...)
for (Submission link : new SubredditPaginator(reddit).next())
    System.out.printf("%s↑ /r/%s - %s\n", link.getScore(), link.getSubreddit(), link.getTitle())
```

```
4069↑ /r/science - Randomized double-blind placebo-controlled trial shows non-celiac gluten sensitivity is indeed real
3541↑ /r/pics - You would think it it would be gold at the end of our irish rainbows.
1883↑ /r/todayilearned - TIL that Wisconsin's only pirate once got the crew of an entire ship drunk then proceeded to throw them all over board. He then took the ship to Chicago and sold all the cargo.
...
```

#Getting Started

Add JRAW to your dependencies:

```groovy
repositories { jcenter() }

dependencies {
    compile "net.dean.jraw:JRAW:$jrawVersion"
}
```

The [Quickstart wiki page](https://github.com/thatJavaNerd/JRAW/wiki/Quickstart) can help you get started.

#Javadoc

{% include javadoc_table.md %}
