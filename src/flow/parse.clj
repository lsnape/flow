(ns flow.parse)

(alias 'f (doto 'flow.core create-ns))


(declare parse-form)

(defn parse-map-vals [m]
  (->> (for [[k v] m]
         [k (parse-form v)])
       (into {})))

(defn parse-node [[tagish possible-attrs & body]]
  (let [tagish (name tagish)
        attrs (when (map? possible-attrs)
                possible-attrs)

        children (if attrs
                   body
                   (cons possible-attrs body))]

    {:type :node

     :tag (second (re-find #"^([^#.]+)" tagish))
     
     :id (second (re-find #"#([^.]+)" tagish))
     
     :classes (concat (for [class-name (map second (re-seq #"\.([^.]+)" tagish))]
                        {:type :static
                         :class-name class-name})
                      (for [class (::f/classes attrs)]
                        {:type :dynamic
                         :class (parse-form class)}))

     :style (parse-map-vals (::f/style attrs))

     :listeners (parse-map-vals (::f/on attrs))
     
     :attrs (parse-map-vals (dissoc attrs ::f/classes ::f/style ::f/on))
     
     :children (map #(parse-form % {:elem? true}) children)}))



(defmulti parse-call
  (fn [call elem?]
    (first call)))

(defmethod parse-call 'let* [[_ bindings & body] elem?]
  {:call-type :let
   :bindings (for [[bind value] (partition 2 bindings)]
               {:bind bind
                :value (parse-form value)})
   :body (if elem?
           (concat (map parse-form (butlast body))
                   [(parse-form (last body) {:elem? true})])
           
           (map parse-form body))})

(defmethod parse-call 'if [[_ test then else] elem?]
  {:call-type :if
   :test (parse-form test)
   :then (parse-form then {:elem? elem?})
   :else (parse-form else {:elem? elem?})})

(defmethod parse-call 'do [[_ & body] elem?]
  {:call-type :do
   :side-effects (butlast body)
   :return (parse-form (last body) {:elem? elem?})})

(defmethod parse-call '<<! [[_ cursor] _]
  {:type :unwrap-cursor
   :cursor cursor})

(defmethod parse-call '!>> [[_ cursor] _]
  {:type :wrap-cursor
   :cursor cursor})

(defmethod parse-call :default [[call-fn & args] elem?]
  {:call-type :call
   :call-fn call-fn
   :args (map parse-form args)})

(defn parse-form [form & [{:keys [elem?]
                           :or {elem? true}}]]
  (cond
   (and elem? (vector? form)) (parse-node form)

   (seq? form) (assoc (parse-call form elem?)
                 :type :call)

   (symbol? form) {:type :symbol
                   :symbol form}

   (map? form) {:type :map
                :map (->> form
                          (map (partial map parse-form))
                          (into {}))}
   
   (coll? form) {:type :coll
                 :coll (->> form
                            (map parse-form)
                            (into (empty form)))}
   
   :else {:type :primitive
          :primitive form}))

