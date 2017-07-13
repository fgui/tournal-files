(ns tournal-files.core
  (:require [me.raynes.fs :as fs])
  (:import [java.io BufferedInputStream FileInputStream]
           [com.drew.imaging ImageMetadataReader]))

(def exif-directory-regex
  (re-pattern (str "(?i)(" (clojure.string/join "|"
                                                ["Exif" "JPEG" "JFIF"
                                                 "Agfa" "Canon" "Casio" "Epson"
                                                 "Fujifilm" "Kodak" "Kyocera"
                                                 "Leica" "Minolta" "Nikon" "Olympus"
                                                 "Panasonic" "Pentax" "Sanyo"
                                                 "Sigma/Foveon" "Sony"]) ")")))

(defn- extract-from-tag
  [tag]
  (into {} (map #(hash-map (.getTagName %) (.getDescription %)) tag)))

(defn exif-for-file
  "Takes an image file (as a java.io.InputStream or java.io.File) and extracts exif information into a map"
  [file]
  (let [metadata (ImageMetadataReader/readMetadata file)
        exif-directories (filter #(re-find exif-directory-regex (.getName %)) (.getDirectories metadata))
        tags (map #(.getTags %) exif-directories)]
    (into {} (map extract-from-tag tags))))

(defn create-meta-dir [path]
  (let [file (clojure.java.io/file path)]
    (reduce #(update %1 (if (.isFile %2) :files :folders) conj (.getName %2))
            {:folders [] :files []} (fs/list-dir path))))

(defn get-date [file]
  (java.util.Date. (fs/mod-time file)))

(defn- exif-ts [file]
  (try (->>
        (exif-for-file file)
        (some (fn [[k v]] (when (clojure.string/starts-with? k "Date") v)))
        (.parse (java.text.SimpleDateFormat. "yyyy:MM:dd hh:mm:ss"))
        .getTime)
       (catch Exception e nil)))

(defn- file-name-ts [file]
  (let [name (first (-> file .getName (clojure.string/split #"\.")))]
    (if (clojure.string/starts-with? name "20")
      (try (.getTime (.parse (java.text.SimpleDateFormat. "yyyyMMdd_hhmmss") name))
           (catch Exception e nil))
      nil)))

(defn- file-mod-ts [file]
  (fs/mod-time file))

(defn- file-ts [file]
  (or
   (exif-ts file)
   (file-name-ts file)
   (file-mod-ts file)))

(defn date-org-location [ts]
  (.format (java.text.SimpleDateFormat. "yyyy/MM/dd")
           (java.util.Date. ts)))

(defn copy-file-date-org [file-from root-to]
  (let [file-ts (file-ts file-from)
        absolute-path-to (str root-to
                              "/"
                              (date-org-location file-ts)
                              "/"
                              (.getName file-from))]
    (fs/copy+ (.getAbsolutePath file-from)
              absolute-path-to)
    (.setLastModified (fs/file absolute-path-to) file-ts)))

(defn copy-date-org-from-to [root-from root-to]
  (let [files (file-seq (fs/file root-from))]
    (for [f files
          :when (fs/file? f)]
      (do
        (print ".")
        (flush)
        (copy-file-date-org f root-to)))))


(defn write-meta! [folder]
  (let [ls (fs/list-dir folder)
        meta {:folders
            (mapv #(.getName %)
                  (filter fs/directory? ls))
            :files
            (mapv #(.getName %)
                  (filter #(and (fs/file? %) (not (fs/hidden? %))) ls))}]
    (println folder)
    (spit (str folder "/.dir.edn")
          (pr-str meta
                  ))
    (doseq [dir (:folders meta)]
      (write-meta! (str folder "/" dir)))
    ))

(comment


  (fs/list-dir "/tmp")
  (write-meta! "/tmp")
  
  (def f (clojure.java.io/file "/tmp/"))
  (.getAbsolutePath f)
  (date-location-file f)
  (take 10 (file-seq f))
  (copy-file-date-org (fs/file "sample.jpg") "/tmp")
  (java.util.Date.
   (file-name-ts (fs/file "sample.mp4"))
                   )

  (def m (exif-for-file (fs/file "sample.JPG")))

  (copy-date-org-from-to "from" "to")


  (doseq [x (range 20)]
    (do (print ".")
        (flush)
        (Thread/sleep 100)
        x))

  (java.util.Date. (file-ts (fs/file "sample.JPG")))
  (exif-ts (fs/file "sample.JPG"))
  (sort (vals m))
  (ImageMetadataReader/readMetadata (fs/file "sample.JPG")))
