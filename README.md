# Minecraft Sketchpad

A playground for drawing things in Minecraft using [Redstone](http://github.com/henrygarner/redstone).

### 3D objects ###

```clojure
	user=> (require '[redstone.client :as mc])
	nil
	
	user=> (def server
	         {:host "localhost"
			 :port 4711})
	#'user/server
	
	user=> (draw-eggs server 100 [wavy-pattern horizontal-stripes vertical-stripes])
	nil
```

![Easter Eggs](https://raw.githubusercontent.com/henrygarner/minecraft-sketchpad/master/doc/images/eggs.png)

### Images & Video ###

```clojure
	user=> (require '[redstone.client :as mc])
	nil
	
	user=> (def server
	         {:host "localhost"
			 :port 4711})
	#'user/server

	user=> (let [position (mc/player-tile-position server)]
	         (draw-movie! "simpsons.mov" position))
```

![Simpsons Title Screen](https://raw.githubusercontent.com/henrygarner/minecraft-sketchpad/master/doc/images/simpsons.png)

## License

Copyright © 2014 Henry Garner

Distributed under the Eclipse Public License version 1.0
