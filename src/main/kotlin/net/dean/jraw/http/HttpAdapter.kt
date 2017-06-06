package net.dean.jraw.http;

interface HttpAdapter {
    var userAgent: UserAgent
    fun execute(r: HttpRequest)

    @Throws(NetworkException::class)
    fun executeSync(r: HttpRequest): HttpResponse
}