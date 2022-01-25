package tw.com

import io.smallrye.mutiny.Uni
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody
import javax.inject.Inject
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import tw.com.MemberRepo.User
import tw.com.MemberSvc.UserView


@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class MemberCtrl {

    @Inject
    lateinit var memberSvc: MemberSvc

    @GET
    @Path("/user")
    fun getUserInfoList(
        @QueryParam("username") username: String?,
    ): Uni<List<UserView>> {
        return memberSvc.getUsers(username)
    }

    data class UserRegisterInfo(
        var userId: String? = null,
        var username: String? = null
    )
    @POST
    @Path("/user")
    fun register(
        @RequestBody userRegisterInfo: UserRegisterInfo?
    ): Response {
        return if(userRegisterInfo != null){
            Response.ok(memberSvc.createUser(userRegisterInfo)).build()
        }else{
            Response.status(400,"username is null").build()
        }
    }

    @GET
    @Path("/checkUserId")
    fun checkUserId(
        @QueryParam("userId") userId: String
    ): Uni<Boolean>{
        return memberSvc.checkUserIdIsExist(userId)
    }
}