document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-service-carousel]").forEach((carousel) => {
        const track = carousel.querySelector("[data-carousel-track]");
        const previous = carousel.querySelector("[data-carousel-previous]");
        const next = carousel.querySelector("[data-carousel-next]");

        if (!track || !previous || !next || track.children.length < 2) return;

        let items = Array.from(track.children);
        let moving = false;

        const makeClone = (item) => {
            const clone = item.cloneNode(true);
            clone.classList.add("carousel-clone");
            clone.setAttribute("aria-hidden", "true");
            clone.querySelectorAll("a, button").forEach((control) => control.setAttribute("tabindex", "-1"));
            return clone;
        };

        const step = () => track.children[1].offsetLeft - track.children[0].offsetLeft;

        const jumpToStart = () => {
            track.style.scrollBehavior = "auto";
            track.scrollLeft = step();
            track.style.scrollBehavior = "smooth";
        };

        const rebuild = () => {
            track.replaceChildren(makeClone(items[items.length - 1]), ...items, makeClone(items[0]));
            jumpToStart();
        };

        const rotate = (direction) => {
            if (moving) return;
            moving = true;

            const amount = step();
            track.scrollTo({
                left: direction === "next" ? amount * 2 : 0,
                behavior: "smooth"
            });

            window.setTimeout(() => {
                if (direction === "next") {
                    items.push(items.shift());
                } else {
                    items.unshift(items.pop());
                }
                rebuild();
                moving = false;
            }, 420);
        };

        rebuild();
        next.addEventListener("click", () => rotate("next"));
        previous.addEventListener("click", () => rotate("previous"));

        let resizeTimer;
        window.addEventListener("resize", () => {
            window.clearTimeout(resizeTimer);
            resizeTimer = window.setTimeout(rebuild, 120);
        });
    });
});
