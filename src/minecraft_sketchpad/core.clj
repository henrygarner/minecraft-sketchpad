(ns minecraft-sketchpad.core
  (:require [redstone.client :as mc]
            [clojure.string :as s]
            [clojure.java.io :refer [input-stream resource reader file]]
            [clojure.java.shell :refer [sh]]
            [me.raynes.fs :refer [temp-file temp-dir]]))

(def server {:host "localhost"
             :port 4711})

(defn sq [x] (* x x))

(defn is-in-shape [x y z]
  (let [r (Math/sqrt (/ (- 1 (Math/pow (/ (- y 0.5) 0.5) 2))
                        (+ (Math/pow 2 (* 3 y)) 4)))]
    (< (+ (sq (- x 0.5))
          (sq (- z 0.5)))
       (sq r))))

(defn wavy-pattern [x y z]
  (let [o (- x 0.5)
        a (- z 0.5)
        t (Math/atan (/ o a))
        s (Math/sin (* t 6))
        i (+ (/ s 15) 0.4)
        j (+ (/ s 30) 0.7)
        k (+ (/ s 60) 0.9)]
    (cond (or (and (>= y (- i 0.1))
                   (<= y (+ i 0.1)))
              (and (>= y (- j 0.05))
                   (<= y (+ j 0.05))))
          :pink-wool
            
          (> (Math/sin (* y 8 Math/PI)) 0.5)
          :white-wool
            
          :else
          :light-blue-wool)))

(defn vertical-stripes [x y z]
  (let [o (- x 0.5)
        a (- z 0.5)
        t (Math/atan (/ o a))
        s (Math/sin (* t 6))]
    (if (> s 0)
      :yellow-wool
      :white-wool)))

(defn horizontal-stripes [x y z]
  (if (> (Math/sin (* (Math/pow y 3) 8 Math/PI)) 0.5)
    :light-blue-wool
    :white-wool))

;; (mc/set-block! server (mc/player-tile-position server) :orange-wool)

(defn draw-egg [x y z size decoration]
  (doseq [x1 (range size)
          y1 (range size)
          z1 (range size)]
    (let [xf (/ (+ x1 0.5) size)
          yf (/ (+ y1 0.5) size)
          zf (/ (+ z1 0.5) size)]
      (if (is-in-shape xf yf zf)
        (let [color (decoration xf yf zf)]
          (mc/set-block! server {:x (+ x x1 (/ size -2))
                                 :y (+ y y1 10)
                                 :z (+ z z1 (/ size -2))} color))))))

(defn position-seq [offset]
  (let [r (map #(* offset %) (range))
        s (interleave r r)]
    (map vector s (drop 1 s)))) 

(defn draw-eggs [server size decorations]
  (let [{:keys [x y z]} (mc/player-tile-position server)]
    (dorun
     (map (fn [decoration [offset-x offset-z]]
            (draw-egg (+ x offset-x) y (+ z offset-z) size decoration))
          decorations (position-seq size)))))

;; (draw-eggs server 100 [wavy-pattern horizontal-stripes vertical-stripes])


(def clojure-colours
  (let [replacements {\B :blue-wool
                      \b :light-blue-wool
                      \G :green-wool
                      \g :lime-wool
                      \space :white-wool
                      \X :air}
        chars (s/replace (slurp (clojure.java.io/resource "clojure.txt")) "\n" "")]
    (-> (map (fn [char] (if (contains? replacements char)
                       (get replacements char)
                       char))
             (seq chars)))))

(defn draw-clojure [{:keys [x y z]}]
  (let [coords (for [y1 (range 100)
                     x1 (range 100)
                    ]
                 [x1 (- 100 y1)])]
    (dorun
     (map (fn [[x1 y1] colour]
            (mc/set-block! server {:x (+ x x1) :y y1 :z z} colour)
            (mc/set-block! server {:x (+ x x1) :y y1 :z z} colour)) coords clojure-colours))))

(comment (draw-clojure (mc/player-tile-position server)))

(defn parse-pixel [[r g b]]
  (map int [r g b]))

(defn byte-seq [reader]
  (let [result (.read reader)]
    (if (= result -1)
      (do (.close reader) nil)
      (lazy-seq (cons result (byte-seq reader))))))

(defn distance-squared [c1 c2]
  "Euclidean distance between two collections considered as coordinates"
  (->> (map - c1 c2) (map #(* % %)) (reduce +)))

(def block-colours 
  {[221 221 221] :wool
   [219 126 63] :orange-wool
   [179 81 188] :magenta-wool
   [108 138 201] :light-blue-wool
   [177 166 40] :yellow-wool
   [66 175 58] :lime-wool
   [208 132 153] :pink-wool
   [65 65 65] :gray-wool
   [154 161 161] :light-gray-wool
   [47 111 137] :cyan-wool
   [127 62 181] :purple-wool
   [47 57 141] :blue-wool
   [80 51 32] :brown-wool
   [54 71 28] :green-wool
   [150 53 49] :red-wool
   [26 23 23] :black-wool})

(defn rgb->block [rgb-triple colour-map]
  (colour-map
   (apply min-key (partial distance-squared rgb-triple) (keys colour-map))))

(defn ppm-reader [byte-seq]
  (let [[magic dimensions max-value pixels] (remove #(-> % first (= 35))
                                                    (take-nth 2 (partition-by #(= % 10) byte-seq)))
        [x y] (map #(Long/parseLong %) (s/split (apply str (map char dimensions)) #" "))]
    (with-meta (map #(rgb->block % block-colours) (partition 3 pixels))
      {:width x :height y})))

(defn read-ppm [path]
  (let [stream (-> path input-stream byte-seq)]
    (ppm-reader stream)))

(defn draw-ppm! [path {:keys [x y z]}]
  (println path)
  (let [image (read-ppm path)
        {:keys [width height]} (meta image)
        blocks (map vector (for [y1 (range height)
                                 x1 (range width)] [x1 (- height y1)]) image)]
     (doseq [[[x1 y1] colour] blocks]
       (mc/set-block! server {:x (+ x x1) :y (+ y y1) :z z} colour))))

(defn image->ppm! [path width]
  (let [tmpfile (str (doto (temp-file "minecraftimage" ".ppm") .deleteOnExit))
        map-path (.getPath (resource "map.png"))
        results (sh "convert" path "-resize" (str width) "-map" map-path tmpfile)]
    tmpfile))

(defn draw-image! [path position]
  (draw-ppm! (image->ppm! path 200) position))

(defn draw-images! [path position]
  (let [directory (file path)]
    (doseq [f (take-nth 10 (drop 1 (file-seq directory)))]
      (draw-image! (str f) position))))

(defn draw-movie! [movie position]
  (let [tmp-dir (temp-dir "minecraft")
        results (sh "ffmpeg" "-i" movie "-y" (str tmp-dir "/%03d.jpg"))]
    (draw-images! (str tmp-dir) position)))

