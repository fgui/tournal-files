(set-env! :dependencies
          '[[me.raynes/fs "1.4.6"]
            [com.drewnoakes/metadata-extractor "2.10.1"]]
          :source-paths #{"src/"})

(task-options!
 pom {:project 'tournal-file
      :version "0.1.0"}
 jar {:main 'tournal-file.core}
 aot {:all true})
