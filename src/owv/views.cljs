(ns owv.views
  (:require [clojure.string :as str]
            [owv.date :refer [days-since days-until format-date]]
            [owv.todo :as t]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

(defn linkify
  "Transform links in org format (e.g. [[http://example.com][click me]]) to
  anchor elements."
  [s]
  (->> s
       (re-seq #"[^\[\]]+|\[\[.+?\]\]")
       (map (fn [part]
              (if-let [[_ url label] (re-matches #"\[\[(.+)\]\[(.+)\]\]" part)]
                [:a {:href url :target "_blank" :key url} label]
                part)))))

;; Configuration

(defn config-panel []
  (let [prev-url @(subscribe [:todos-url])
        url (r/atom prev-url)]
    (fn []
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (dispatch [:set-todos-url @url]))}
       [:div
        [:label {:for "todo-json-url"} "Data URL: "]
        [:input#todo-json-url {:type "text"
                               :value @url
                               :on-change #(reset! url (.-value (.-target %)))}]]
       [:div
        [:input {:type "submit"
                 :disabled (nil? @url)
                 :value "OK"}]
        [:button {:disabled (nil? prev-url)
                  :on-click #(do
                               (.preventDefault %)
                               (dispatch [:show-settings-pane]))}
         "Cancel"]]])))

;; Capture new to-dos

(defn selected-values [select]
  (doall (->> (.-options select)
              (filter #(.-selected %))
              (map #(.-value %)))))

(defn new-todo-panel []
  (let [new-todo (r/atom {:tags () :headline ""})
        all-tags @(subscribe [:tags])]
    (fn []
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (dispatch [:add-todo @new-todo]))}
       [:div
        [:label {:for "new-todo-tags"} "Tag(s):"]
        [:select#new-todo-tags {:multiple true
                                :on-change #(swap! new-todo assoc :tags (selected-values (.-target %)))
                                :value (:tags @new-todo)}
         (for [tag all-tags]
           [:option {:key tag :value tag} tag])]]
       [:div
        [:label {:for "new-todo-headline"} "Headline:"]
        [:input#new-todo-headline {:on-change #(swap! new-todo assoc :headline (.-value (.-target %)))
                                   :value (:headline @new-todo)}]]
       [:div
        [:input {:type "submit"
                 :disabled (str/blank? (:headline @new-todo))
                 :value "OK"}]
        [:button {:on-click #(do
                               (.preventDefault %)
                               (dispatch [:show-settings-pane]))}
         "Cancel"]]])))

;; Review new/completed to-dos

(defn todos-review-list [{:keys [todos action-label on-click]}]
  [:ul
    (for [t todos]
      [:li {:key (:headline t)}
       (linkify (:headline t))
       (if-let [tags (seq (:tags t))]
         (str " (" (str/join ", " tags) ")"))
       " "
       [:button {:on-click #(on-click t)} action-label]])])

(defn review-todos-panel [todos-list]
  [:div
   todos-list
   [:div
    [:button {:on-click #(dispatch [:show-settings-pane])}
     "Done"]]])

(defn new-todos-panel []
  [:<>
   [:div "New to-dos"]
   [review-todos-panel
    [todos-review-list {:todos @(subscribe [:new-todos])
                        :action-label "delete"
                        :on-click #(dispatch [:delete-todo %])}]]])

(defn completed-todos-panel []
  [:<>
   [:div "Completed to-dos"]
   [review-todos-panel
    [todos-review-list {:todos @(subscribe [:completed-todos])
                        :action-label "restore"
                        :on-click #(dispatch [:uncomplete-todo %])}]]])

;; Settings pane

(defn settings-pane []
  [:div.settings-panel
   (condp = @(subscribe [:visible-settings-panel])
     :new-todo [new-todo-panel]
     :new-todos [new-todos-panel]
     :completed-todos [completed-todos-panel]
     :config [config-panel]
     [:div
      [:ul.settings-menu
       [:li [:button {:on-click #(dispatch [:show-new-todo-panel])} "New to-do"]]
       (let [new-todo-count (count @(subscribe [:new-todos]))]
         [:li [:button {:on-click #(dispatch [:show-new-todos-panel])
                        :disabled (zero? new-todo-count)}
               "Review new to-dos (" new-todo-count ")"]])
       (let [completed-todo-count (count @(subscribe [:completed-todos]))]
         [:li [:button {:on-click #(dispatch [:show-completed-todos-panel])
                        :disabled (zero? completed-todo-count)}
               "Review completed to-dos (" completed-todo-count ")"]])
       [:li [:button {:on-click #(dispatch [:show-config-panel])} "Configure"]]]
      [:div.updated-at
       "Updated " [:span {:title @(subscribe [:last-updated-at])}
                   (str @(subscribe [:last-updated-minutes-ago]) " minutes ago")]]])])

;; To-do list

(defn todo-item [_]
  (let [marked-complete? (r/atom false)
        hide-timeout (atom nil)]
    (fn [{{:keys [state headline created scheduled deadline] :as todo} :todo}]
      [:details.todo-item
       {:class [(if (t/stale? todo) "stale")
                (if @marked-complete? "complete")
                (cond (t/overdue? todo) "overdue"
                      (t/needs-attention? todo) "needs-attention"
                      (t/ready? todo) "ready")]}
       [:summary
        [:input.todo-checkbox
         {:type "checkbox"
          :on-click (fn [_]
                      (if @marked-complete?
                        (do
                          (.clearTimeout js/window @hide-timeout)
                          (reset! hide-timeout nil)
                          (reset! marked-complete? false))
                        (do
                          (reset! hide-timeout (.setTimeout js/window #(dispatch [:complete-todo todo]) 1000))
                          (reset! marked-complete? true))))}]
        (linkify headline)
        (if created
          [:span.created "(" (days-since created) " days old)"])]
       [:div.todo-meta
        [:div "Created: " (if created (format-date created) "?")]
        (if scheduled [:div "Scheduled: " (format-date scheduled)])
        (if deadline [:div "Deadline: " (format-date deadline)])]])))

(defn todo-group [tag]
  (let [todos @(subscribe [:tagged-todos tag])
        ready @(subscribe [:tagged-ready-todos tag])
        needing-attention @(subscribe [:tagged-todos-needing-attention tag])
        overdue @(subscribe [:tagged-overdue-todos tag])]
    [:details.todo-group
     [:summary.todo-group-header
      [:span.todo-tag tag]
      (cond
        (> (count overdue) 0) [:span.todo-group-status-indicator.overdue]
        (> (count needing-attention) 0) [:span.todo-group-status-indicator.needs-attention]
        (> (count ready) 0) [:span.todo-group-status-indicator.ready])]
     (for [todo todos]
       [todo-item {:key (:headline todo) :todo todo}])]))

(defn todo-list []
  (let [tags @(subscribe [:tags])]
    [:<>
     (for [tag tags]
       ^{:key tag} [todo-group tag])]))

;; App root

(defn loading-indicator []
  "Loading ...")

(defn header []
  (let [show-settings-pane? @(subscribe [:show-settings-pane?])]
    [:div.page-header
     [:button {:on-click #(if show-settings-pane?
                            (dispatch [:hide-settings-pane])
                            (dispatch [:show-settings-pane]))}
      "Settings"]]))

(defn root []
  (let [show-settings-pane? @(subscribe [:show-settings-pane?])
        loading-todos? @(subscribe [:loading-todos?])]
    [:<>
     [header]
     (if show-settings-pane? [settings-pane])
     (if loading-todos? [loading-indicator])
     [todo-list]]))
