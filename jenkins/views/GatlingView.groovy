sectionedView('Gatling') {
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
        listView {
            name('Test Conduction')
            jobs {
                regex(/Load.Test.*/)
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