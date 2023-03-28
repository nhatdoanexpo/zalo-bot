## Requirements

- Java 19
- Chrome (latest version)

## EXAMPLE

post message structure
```json
{
  "messageStatus": "NEW",
  "message": "hello"
}
```

```javascript
const messageObj = {
  "messageStatus": "NEW",
  "message": "hello"
};

window.parent.postMessage(JSON.stringify(messageObj), '*');

// FAKE BE listener 
document.querySelector('body').addEventListener('CLIENT_IN', function(event) {
    console.log(event.detail);
    const data = event.detail; // { detail: { actionType: 'SEND_KEY' , data: { selector: "#input" , value: "Input data" } } }
    // lam gi do

    // sau khi lam xong 
    data.status = 'DONE'; // update trang thai
    document.querySelector('body').dispatchEvent(new CustomEvent('CLIENT_OUT', { detail: data })); // thong bao lam xong
});

// FE sendEvent
function sendEvent(data) {
    return new Promise((resolve) => {
        document.querySelector('body').addEventListener('CLIENT_OUT', function(event) {
            resolve(event.detail);
        });
        document.querySelector('body').dispatchEvent(new CustomEvent('CLIENT_IN', { detail: data }));
    });
}

const model = { actionType: 'SEND_KEY' , data: { selector: "#input" , value: "Input data" } };
sendEvent(model).then( data => {
    console.log(data);
})



```

