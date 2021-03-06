(ns flow.holder
  (:require [flow.dom :as fd]
            [flow.diff :refer [vector-diff]]))

(defprotocol ElementHolder
  (append-to! [_ $parent])
  (swap-child! [_ $new-el]))

(defn swap-elem-seqs! [old-els new-els]
  (let [$last-elem (last old-els)
        $next-sibling (.-nextSibling $last-elem)
        $parent (.-parentNode $last-elem)

        diff (vector-diff old-els new-els)]

    (doseq [[action $el] diff]
      (when (= action :moved-out)
        (fd/remove! $el)))

    (reduce (fn [$next-sibling [action $new-el]]
              (case action
                :kept $new-el
                :moved-in (do
                            (fd/insert-child-before! $parent $new-el $next-sibling)
                            $new-el)
                :moved-out $next-sibling))
            
            $next-sibling
            (reverse diff))

    new-els))

(defn new-multi-holder [$els]
  (let [!current-els (atom $els)]
    (reify ElementHolder
      (append-to! [_ $parent]
        (fd/append-child! $parent @!current-els))
      
      (swap-child! [this $new-el]
        (let [$new-el (fd/->node (if (seq? $new-el)
                                   (seq $new-el)
                                   $new-el))]
          (if-not (seq? $new-el)
            (let [$first-el (first @!current-els)]
              (fd/insert-child-before! (.-parentNode $first-el)
                                       $new-el
                                       $first-el)
              (doseq [$el @!current-els]
                (fd/remove! $el))
              $new-el)

            (do
              (reset! !current-els (swap-elem-seqs! @!current-els $new-el))
              this)))))))

(defn upgrade-to-holder [$old-el $new-els]
  (let [$parent (.-parentNode $old-el)]
    (doseq [$new-el $new-els]
      (fd/insert-child-before! $parent $new-el $old-el)))
  
  (fd/remove! $old-el)
  (new-multi-holder $new-els))

(extend-protocol ElementHolder
  js/Node
  (append-to! [$el $parent]
    (fd/append-child! $parent $el))

  (swap-child! [$el $new-elem]
    (let [$new-elem (fd/->node $new-elem)]
      (if (seq? $new-elem)
        (upgrade-to-holder $el $new-elem)

        (let [$parent (.-parentNode $el)]
          (.replaceChild $parent $new-elem $el)
          $new-elem)))))

(defn new-element-holder [$el-or-$els]
  (let [$el-or-$els (fd/->node $el-or-$els)]
    (if (seq? $el-or-$els)
      (new-multi-holder $el-or-$els)
      $el-or-$els)))
