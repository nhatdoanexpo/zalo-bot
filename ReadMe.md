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

```