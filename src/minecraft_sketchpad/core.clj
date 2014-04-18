(ns minecraft-sketchpad.core
  (:require [redstone.client :as mc]))

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



