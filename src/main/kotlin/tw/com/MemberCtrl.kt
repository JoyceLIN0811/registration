package tw.com

import org.bson.types.ObjectId
import javax.inject.Inject
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody


@Path("/api")
class MemberCtrl {

    @Inject
    lateinit var memberSvc: MemberSvc

    @POST
    @Path("/user")
    fun addMaterial(
        @RequestBody username: String?
    ): Response {
        return if(!username.isNullOrBlank()){
            memberSvc.createUser(username)
            Response.ok().build()
        }else{
            Response.status(400,"username is null").build()
        }
    }
}