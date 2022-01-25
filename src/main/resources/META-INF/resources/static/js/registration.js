var app = new Vue({
    el: '#userRegistrationPage',
    data: {
        userId: '',
        isAvailableUserId: null,
        username: '',
    },
    methods:{
        initPage: function (){
            this.userId = ""
            this.username = ""
            this.isAvailableUserId = null
        },
        register: function (){
            if(this.userId.trim().length === 0) {
                alert("帳號不能為空");
                return;
            }
            if(this.username.trim().length === 0) {
                alert("名稱不能為空");
                return
            }
            if(!this.isAvailableUserId){
                alert("該帳號已註冊過");
                return
            }
            let data = {}
            data.userId = this.userId
            data.username = this.username

            fetch("/api/user",{
                method: 'post',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            }).then(res => {
                if (res.ok) {
                    return res.json()
                }else {
                    $('#modal-message').html("註冊失敗")
                }
            }).then( result =>{
                let username = result.username
                let userId = result.userId
                $('#modal').modal('show')
                $('#modal-message').html(`使用者 ${userId} 註冊名稱：${username} 成功`)
                this.initPage()
            })
        },
        checkIsExistsUserId: function (){
            if(this.userId.trim().length > 0) {
                fetch(`/api/checkUserId?userId=${this.userId}`,{
                }).then(res => {
                    if (res.ok) {
                        return res.json()
                    }
                }).then( result =>{
                    this.isAvailableUserId = !!result;
                })
            }else {
                this.isAvailableUserId = null
            }
        }
    }
});