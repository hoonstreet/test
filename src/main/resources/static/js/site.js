document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-service-carousel]").forEach((carousel) => {
        const track = carousel.querySelector("[data-carousel-track]");
        const previous = carousel.querySelector("[data-carousel-previous]");
        const next = carousel.querySelector("[data-carousel-next]");

        if (!track || !previous || !next || track.children.length < 2) return;

        const originals = Array.from(track.children);
        const firstClone = originals[0].cloneNode(true);
        const lastClone = originals[originals.length - 1].cloneNode(true);

        [firstClone, lastClone].forEach((clone) => {
            clone.classList.add("carousel-clone");
            clone.setAttribute("aria-hidden", "true");
            clone.querySelectorAll("a, button").forEach((item) => item.setAttribute("tabindex", "-1"));
        });

        track.prepend(lastClone);
        track.append(firstClone);

        const step = () => track.children[1].offsetLeft - track.children[0].offsetLeft;
        const jumpTo = (position) => {
            track.style.scrollBehavior = "auto";
            track.scrollLeft = position;
            track.style.scrollBehavior = "smooth";
        };

        requestAnimationFrame(() => jumpTo(step()));

        next.addEventListener("click", () => {
            const amount = step();
            track.scrollBy({ left: amount, behavior: "smooth" });
            window.setTimeout(() => {
                if (track.scrollLeft >= amount * (originals.length + 1) - 2) jumpTo(amount);
            }, 420);
        });

        previous.addEventListener("click", () => {
            const amount = step();
            track.scrollBy({ left: -amount, behavior: "smooth" });
            window.setTimeout(() => {
                if (track.scrollLeft <= 2) jumpTo(amount * originals.length);
            }, 420);
        });

        let resizeTimer;
        window.addEventListener("resize", () => {
            window.clearTimeout(resizeTimer);
            resizeTimer = window.setTimeout(() => jumpTo(step()), 120);
        });
    });
});
