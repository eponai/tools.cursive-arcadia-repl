# tools.cursive-arcadia-repl
An [nREPL] bridge for [Arcadia Unity]. Heavily inspired by [arcadia.nrepl](https://github.com/spacepluk/arcadia.nrepl),
with the difference of redirecting eval operations to Arcadia's nREPL rather than its UDP REPL. Cursive specific operations
are not redirected but evaluated in the bridge as they require the JVM.

[nREPL]: https://github.com/clojure/tools.nrepl
[Arcadia Unity]: https://github.com/arcadia-unity/Arcadia

### How to use Cursive with Arcadia

In your Unity project with Arcadia, do:

1. Create a new "regular" looking clojure project that has a deps.edn at its root. For example:

  ```sh
cd Assets
mkdir <project-name>
cd <project-name>
mkdir src test target
echo '{:deps {org.clojure/clojure {:mvn/version "1.10.0"}
              arcadia             {:local/root "../Arcadia"}}}' > deps.edn
```

2. Open and import the project with Cursive at that root.
3. You should now have auto complete with the arcadia and clojure-clr sources.

Now we'll want to talk to Arcadia via nREPL. To do this:

1. Clone this repository: git@github.com:eponai/tools.cursive-arcadia-repl.git
2. cd tools.cursive-arcadia-repl
3. Either run `lein run` or `clj -A:repl`

An nREPL bridge has now been created on the default port 7888. Create a remote REPL in Cursive with this port.

When that's all set up:

* Add `(:require [arcadia.core :as arc])` to one of your clojure files.
* Load the file and send `(arc/log "hi!")` to the remote REPL.
* You should see `"hi!"` being printed in Unity's console!

Now you can start developing your game!
