<script>
    import { sanitize } from "../services/utils.js";

    let { htmlContent } = $props();

    let cleanHtml = $derived(sanitize(htmlContent));
</script>

<!-- Note that cleanHtml is ALWAYS sanitized -->
<!-- svelte-ignore security-anchor-html -->
<!-- svelte-doctor-disable-next-line xss -->
{@html cleanHtml}