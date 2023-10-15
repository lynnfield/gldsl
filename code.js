class Rectangle {
    constructor(x, y, width, height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.top = y;
        this.bottom = y + height;
        this.left = x;
        this.right = x + width;
    }

    contains(x, y) {
        return this.left < x && x < this.right && this.top < y && y < this.bottom;
    }

    moveTo(x, y) {
        this.x = x;
        this.y = y;

        this.top = y;
        this.bottom = y + this.height;
        this.left = x;
        this.right = x + this.width;
    }

    resizeTo(handle, x, y) {
        switch (handle) {
            case 'left':
                this.x = x;
                this.left = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.left = this.right - 5;
                    this.x = this.left;
                }
                break;
            case 'right':
                this.right = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.right = this.left + 5;
                }
                break;
            case 'top':
                this.y = y;
                this.top = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.top = this.bottom - 5;
                    this.y = this.top;
                }
                break;
            case 'bottom':
                this.bottom = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.bottom = this.top + 5;
                }
                break;
            case 'topleft':
                this.y = y;
                this.top = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.top = this.bottom - 5;
                    this.y = this.top;
                }
                this.x = x;
                this.left = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.left = this.right - 5;
                    this.x = this.left;
                }
                break;
            case 'bottomleft':
                this.bottom = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.bottom = this.top + 5;
                }
                this.x = x;
                this.left = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.left = this.right - 5;
                    this.x = this.left;
                }
                break;
            case 'topright':
                this.y = y;
                this.top = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.top = this.bottom - 5;
                    this.y = this.top;
                }
                this.right = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.right = this.left + 5;
                }
                break;
            case 'bottomright':
                this.bottom = y;
                this.height = this.bottom - this.top;
                if (this.height < 5) {
                    this.height = 5;
                    this.bottom = this.top + 5;
                }
                this.right = x;
                this.width = this.right - this.left;
                if (this.width < 5) {
                    this.width = 5;
                    this.right = this.left + 5;
                }
                break;
        }

    }
}

function drawRectangle(ctx, rectangle) {
    ctx.strokeRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
}

function addContextMenu(canvas, callbacks) {
    const contextMenu = document.getElementById("context-menu");
    const closeContextMenu = () => {
        contextMenu.style.display = "none";
        while (contextMenu.firstChild) {
            contextMenu.removeChild(contextMenu.lastChild);
        }
    };
    canvas.addEventListener("contextmenu", (e) => {
        console.log(e);
        contextMenu.style.top = e.clientY + "px";
        contextMenu.style.left = e.clientX + "px";
        contextMenu.style.display = "block";

        const createNewBlockButton = createButtonCreateNewBlock(() => {
            callbacks.onCreateNewBlock(e.clientX, e.clientY);
            closeContextMenu();
        });

        contextMenu.appendChild(createNewBlockButton);

        e.preventDefault();
    });
    canvas.addEventListener("click", closeContextMenu);
}

function createButtonCreateNewBlock(onCreateNewBlock) {
    const button = document.createElement('button');
    button.textContent = 'Create new block';
    button.addEventListener('click', (e) => {
        onCreateNewBlock();
        e.preventDefault();
    });
    return button;
}


function findBlock(blocks, x, y) {
    for (const block of blocks) {
        if (block.contains(x, y)) {
            return block;
        }
    }
    return null;
}

class DraggingRectangle {
    constructor(rectangle, offsetX, offsetY) {
        this.rectangle = rectangle;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    moveTo(x, y) {
        this.rectangle.moveTo(x - this.offsetX, y - this.offsetY);
    }
}

function addDragFeature(canvas, blocks, redraw) {
    var draggableBlock = null;

    canvas.addEventListener('mousedown', (e) => {
        const mouseX = e.clientX - canvas.getBoundingClientRect().left;
        const mouseY = e.clientY - canvas.getBoundingClientRect().top;

        const block = findBlock(blocks, mouseX, mouseY)

        if (block) {
            draggableBlock = new DraggingRectangle(
                block,
                mouseX - block.x,
                mouseY - block.y
            );
            console.log('drag block', draggableBlock);
        } else {
            console.log('block not found at (' + mouseX + ',' + mouseY + ')', blocks);
        }
    });

    canvas.addEventListener('mouseup', () => {
        if (draggableBlock) {
            console.log('release block', draggableBlock);
            draggableBlock = null;
        }
    });

    canvas.addEventListener('mousemove', (e) => {
        if (draggableBlock) {
            const mouseX = e.clientX - canvas.getBoundingClientRect().left;
            const mouseY = e.clientY - canvas.getBoundingClientRect().top;

            draggableBlock.moveTo(mouseX, mouseY);

            redraw();
        }
    });
}

function findBlockBorder(blocks, x, y) {
    for (const block of blocks) {
        if (block.top == y) {
            if (block.left == x) {
                return { handle: 'topleft', block: block };
            } else if (block.right == x) {
                return { handle: 'topright', block: block };
            } else {
                return { handle: 'top', block: block };
            }
        } else if (block.right == x) {
            if (block.top == y) {
                return { handle: 'topright', block: block };
            } else if (block.bottom == y) {
                return { handle: 'bottomright', block: block };
            } else {
                return { handle: 'right', block: block };
            }
        } else if (block.bottom == y) {
            if (block.left == x) {
                return { handle: 'bottomleft', block: block };
            } else if (block.right == x) {
                return { handle: 'bottomright', block: block };
            } else {
                return { handle: 'bottom', block: block };
            }
        } else if (block.left == x) {
            if (block.top == y) {
                return { handle: 'topleft', block: block };
            } else if (block.bottom == y) {
                return { handle: 'bottomleft', block: block };
            } else {
                return { handle: 'left', block: block };
            }
        }
    }
    return null;
}

class ResizingRectangle {
    constructor(rectangle, handle) {
        this.rectangle = rectangle;
        this.handle = handle;
    }

    resizeTo(x, y) {
        this.rectangle.resizeTo(this.handle, x, y);
    }
}

function addResizeFeature(canvas, blocks, redraw) {
    var resizingBlock = null;

    canvas.addEventListener('mousedown', (e) => {
        const mouseX = e.clientX - canvas.getBoundingClientRect().left;
        const mouseY = e.clientY - canvas.getBoundingClientRect().top;

        const foundBlockBorder = findBlockBorder(blocks, mouseX, mouseY);

        if (foundBlockBorder) {
            resizingBlock = new ResizingRectangle(
                foundBlockBorder.block,
                foundBlockBorder.handle,
            );
            console.log('resize block', foundBlockBorder);
        } else {
            console.log('block border not found at (' + mouseX + ',' + mouseY + ')', blocks);
        }
    });

    canvas.addEventListener('mouseup', () => {
        if (resizingBlock) {
            console.log('release block', resizingBlock);
            resizingBlock = null;
        }
    });

    canvas.addEventListener('mousemove', (e) => {
        if (resizingBlock) {
            const mouseX = e.clientX - canvas.getBoundingClientRect().left;
            const mouseY = e.clientY - canvas.getBoundingClientRect().top;

            resizingBlock.resizeTo(mouseX, mouseY);

            redraw();
        }
    });
}

function draw(ctx, blocks) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (const block of blocks) {
        drawRectangle(ctx, block);
    }
}

function init() {
    const canvas = document.getElementById("canvas");
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    const ctx = canvas.getContext("2d");
    const blocks = [];

    function redraw() {
        draw(ctx, blocks)
    }

    addContextMenu(
        canvas,
        {
            onCreateNewBlock: (x, y) => {
                blocks.push(new Rectangle(x, y, 50, 50));
                console.log(blocks);
                redraw();
            }
        }
    );

    addDragFeature(canvas, blocks, () => redraw());

    addResizeFeature(canvas, blocks, () => redraw());

    redraw();
}

window.onload = init;
