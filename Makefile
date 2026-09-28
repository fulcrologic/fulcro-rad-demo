cljs:
	pnpm exec shadow-cljs -A:f3-dev:rad-dev:i18n-dev server

report:
	pnpm exec shadow-cljs run shadow.cljs.build-report main report.html

release:
	TIMBRE_LEVEL=:warn pnpm exec shadow-cljs release main

server:
	clj -A:dev:datomic -M -m com.example.components.server