cljs:
	shadow-cljs -A:f3-dev:rad-dev:i18n-dev watch main

report:
	npx shadow-cljs run shadow.cljs.build-report main report.html
