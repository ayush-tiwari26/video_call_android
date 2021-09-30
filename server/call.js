let localVideo = document.getElementById("local-video")
let remoteVideo = document.getElementById("remote-video")

localVideo.style.opacity = 0
remoteVideo.style.opacity = 0

localVideo.onplaying = () => {localVideo.style.opacity = 1}
remoteVideo.onplaying = () => {remoteVideo.style.opacity = 1}

let peer 
function init(userId) {
    peer = new Peer(userId, {
        host: '192.168.29.41',
        port: 9000,
        path: '/.'
    })
    peer.on('open', ()=>{
        //Make a call to a kotlin function in android
    })
    listen()
}

let localStream
function listen(){
    peer.on('call', (call)=>{
        navigator.getUserMedia({
            audio: true,
            video: true
        }, (stream) => {
            localVideo.srcObject = stream
            localStream = stream

            call.answer(stream)
            call.on('stream',(remoteStream)=>{
                remoteVideo.srcObject = remoteStream
                
                remoteVideo.className = "primary-video"
                localVideo.className = "secondary-video"
            })
        })
    })
}

function startCall(otherUserId){
    navigator.getUserMedia({
        audio: true,
        video: true
    },(stream =>{

        localVideo.serObject = stream
        localStream = stream

        const call = peer.call(otherUserId, stream)

        call.on('stream',(remoteStream)=>{
            remoteVideo.srcObject = remoteStream
            
            remoteVideo.className = "primary-video"
            localVideo.className = "secondary-video"
        })
    }))
}

//function to mute the video track
function toggleVideo(b){
    if(b=="true"){
        localStream.getVideoTracks()[0].enabled = true
    }else{
        localStream.getVideoTracks()[0].enabled = false
    }
}

//function to mute the audio track
function toggleAudio(b){
    if(b=="true"){
        localStream.getAudioTracks()[0].enabled = true
    }else{
        localStream.getAudioTracks()[0].enabled = false
    }
}