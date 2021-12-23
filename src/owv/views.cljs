(ns owv.views
  (:require [clojure.string :as str]
            [owv.date :refer [days-since days-until format-date]]
            [owv.todo :as t]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

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
                               (dispatch [:hide-config-panel]))}
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
                               (dispatch [:hide-new-todo-panel]))}
         "Cancel"]]])))

;; Review new to-dos

(defn review-new-todos-panel []
  (let [new-todos @(subscribe [:new-todos])]
    [:div
     [:ul
      (for [t new-todos]
        [:li {:key (:headline t)}
         (:headline t)
         (if-let [tags (seq (:tags t))]
           (str " (" (str/join ", " tags) ")"))
         " "
         [:button {:on-click #(dispatch [:delete-todo t])} "delete"]])]
     [:div
      [:button {:on-click #(dispatch [:hide-review-new-todos-panel])}
       "Done"]]]))

;; Settings pane

(defn settings-pane []
  [:div.settings-panel
   (condp = @(subscribe [:visible-settings-panel])
     :new-todo [new-todo-panel]
     :review-new-todos [review-new-todos-panel]
     :config [config-panel]
     [:div
      [:ul.settings-menu
       [:li [:button {:on-click #(dispatch [:show-new-todo-panel])} "New to-do"]]
       [:li [:button {:on-click #(dispatch [:show-review-new-todos-panel])}
             "Review new to-dos (" (count @(subscribe [:new-todos])) ")"]]
       [:li [:button "Review completed to-dos"]]
       [:li [:button {:on-click #(dispatch [:show-config-panel])} "Configure"]]]
      [:div.updated-at
       "Updated " [:span {:title @(subscribe [:last-updated-at])}
                   (str @(subscribe [:last-updated-minutes-ago]) " minutes ago")]]])])

;; To-do list

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
     (for [{:keys [state headline created scheduled deadline] :as todo} todos]
       [:details.todo-item
        {:key headline
         ;; TODO: figure out which things we want to see here
         ;; (e.g. waiting, deferred?)
         :data-todo-state (str/lower-case state)
         :class [(if (t/stale? todo) "stale")
                 (cond (t/overdue? todo) "overdue"
                       (t/needs-attention? todo) "needs-attention"
                       (t/ready? todo) "ready")]}
        [:summary
         [:input.todo-checkbox {:type "checkbox"}]
         (linkify headline)
         (if created
           [:span.created "(" (days-since created) " days old)"])]
        [:div.todo-meta
         [:div "Created: " (if created (format-date created) "?")]
         (if scheduled [:div "Scheduled: " (format-date scheduled)])
         (if deadline [:div "Deadline: " (format-date deadline)])]])]))

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
