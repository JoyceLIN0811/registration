var app = new Vue({
    el: '#userRegistrationPage',
    data: {
        userId: '',
        username: '',
    },
    methods:{
        register: function (){
            if(this.userId.trim().length === 0) {
                alert("帳號不能為空");
                return;
            }
            if(this.username.trim().length === 0) {
                alert("名稱不能為空");
                return
            }
            let data = {}
            data.userId = this.userId
            data.username = this.username

            fetch("/api/user",{
                method: 'post',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            }).then(res => {
                console.log("res=",res)
                // if (res.ok) {
                //     return res.json()
                // }else {
                //     $('#modal-message').html("註冊失敗")
                // }
            })
            //     .then( result =>{
            //     let username = result.username
            //     let userId = result.userId
            //     $('#modal').modal('show')
            //     $('#modal-message').html(`${username} 註冊成功`)
            // })

        }
    }
});