sectionedView('Build') {
    filterBuildQueue()
    filterExecutors()
    sections {
        listView {
            name('Release')
            jobs {
                regex(/Release.*/)
            }
            columns {
                status()
                weather()
                name()
                lastSuccess()
                lastFailure()
                lastDuration()
                buildButton()
            }
        }
    }
}