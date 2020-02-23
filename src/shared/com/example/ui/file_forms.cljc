(ns com.example.ui.file-forms
  (:require
    [com.example.model.file :as m.file]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form FileForm [this props]
  {::form/id           m.file/id
   ::form/attributes   [m.file/uploaded-on
                        m.file/sha
                        m.file/filename]
   ::form/field-labels {:file.sha/filename ""
                        :file/uploaded-on  ""
                        :file/sha          ""}})
