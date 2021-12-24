(ns owv.events
  (:require [clojure.set :as set]
            [owv.local-storage :as ls]
            [owv.todo-api :as todo-api]
            [re-frame.core :refer [after inject-cofx reg-event-db reg-event-fx]]))

(def url-key "owv.todos-url")
(def new-todos-key "owv.new-todos")
(def completed-todos-key "owv.completed-todos")

(def persist-db
  (after (fn [db]
           (doseq [[db-key persisted-key] {:todos-url url-key
                                           :new-todos new-todos-key
                                           :completed-todos completed-todos-key}]
             (ls/write persisted-key (get db db-key))))))

(reg-event-fx :init
  [(inject-cofx ::ls/read {url-key ::url
                           new-todos-key ::new-todos
                           completed-todos-key ::completed-todos})]
  (fn [{:keys [::url ::new-todos ::completed-todos]} _]
    {:db {:todos-url url
          :new-todos new-todos
          :completed-todos (or completed-todos #{})}
     :fx [[:dispatch (if url
                       [:load-todos url]
                       [:show-config-panel])]]}))

(reg-event-fx :set-todos-url
  [persist-db]
  (fn [{:keys [db]} [_ url]]
    {:db (assoc db :todos-url url)
     :fx [[:dispatch [:hide-settings-pane]] ; does this belong here?
          [:dispatch [:load-todos url]]]}))

(reg-event-fx :load-todos
  (fn [{:keys [db]} [_ url]]
    {:db (assoc db :loading-todos? true)
     ::todo-api/fetch url}))

(reg-event-db :todos-loaded
  [persist-db]
  (fn [db [_ {:keys [todos updated-at]}]]
    (-> db
        (assoc :todos todos
               :updated-at updated-at
               :loading-todos? false)
        ;; clear out any completed todos that no longer exist
        (update :completed-todos set/intersection (set todos)))))

(reg-event-fx :add-todo
  [persist-db]
  (fn [{:keys [db]} [_ todo]]
    {:db (update db :new-todos conj todo)
     :fx [[:dispatch [:show-settings-pane]]]}))

(reg-event-db :delete-todo
  [persist-db]
  (fn [db [_ todo]]
    (update db :new-todos (fn [new-todos]
                            (remove #(= % todo) new-todos)))))

(reg-event-db :complete-todo
  [persist-db]
  (fn [db [_ todo]]
    (update db :completed-todos conj todo)))

(reg-event-db :uncomplete-todo
  [persist-db]
  (fn [db [_ todo]]
    (update db :completed-todos disj todo)))

(doseq [[event visible-panel] {:show-settings-pane :initial
                               :show-new-todo-panel :new-todo
                               :show-new-todos-panel :new-todos
                               :show-completed-todos-panel :completed-todos
                               :show-config-panel :config}]
  (reg-event-db event #(assoc % :settings-panel visible-panel)))

(reg-event-db :hide-settings-pane
  #(dissoc % :settings-panel))
