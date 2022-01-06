sectionedView('Build') {
    filterBuildQueue()
    filterExecutors()
    sections {
        listView {
            name('Image Build')
            jobs {
                regex(/Build.Image.*/)
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