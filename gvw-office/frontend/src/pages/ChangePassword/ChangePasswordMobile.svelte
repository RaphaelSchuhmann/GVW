<script>
    import { push } from "svelte-spa-router";
    import Logo from "../../assets/logo.svg";
    import Input from "../../components/Input.svelte";
    import Button from "../../components/Button.svelte";
    import ToastStack from "../../components/ToastStack.svelte";
    import { getValue, setValue, clearValue } from "../../services/store";
    import { user } from "../../stores/user.svelte";
    import { addToast } from "../../stores/toasts.svelte";
    import { changePassword } from "../../services/changePasswordService.svelte";
    import { normalizeResponse } from "../../api/http.svelte";
    import { handleGlobalApiError } from "../../api/globalErrorHandler.svelte";

    let { message = "" } = $props();

    let isFetching = $state(false);

    let currentPw = "";
    let newPw = "";
    let confirmNewPw = "";

    /**
     * Validates password change form inputs
     */
    function validateInputs({ currentPw, newPw, confirmNewPw }) {
        if (!currentPw) {
            addToast({ title: "Aktuelles Passwort fehlt", type: "error" });
            return true;
        }
        if (!newPw) {
            addToast({ title: "Neues Passwort fehlt", type: "error" });
            return true;
        }
        if (!confirmNewPw) {
            addToast({ title: "Bestätigung fehlt", type: "error" });
            return true;
        }
        if (newPw.length < 8) {
            addToast({ title: "Neues Passwort ist zu kurz", type: "error" });
            return true;
        }
        if (newPw !== confirmNewPw) {
            addToast({ title: "Passwörter stimmen nicht überein", type: "error" });
            return true;
        }
        return false;
    }

    /**
     * Handles the complete password update flow
     */
    async function updatePassword() {
        const hasValidationError = validateInputs({
            currentPw,
            newPw,
            confirmNewPw
        });

        if (hasValidationError) return;

        let email = user.email;
        if (!email || email.length === 0) {
            await push(`/?cpwErr=true`);
            Object.assign(user, { name: "", email: "", role: "", loaded: false });
            return;
        }

        if (isFetching) return;
        isFetching = true;

        try {
            const { resp } = await changePassword(email, currentPw, newPw);
            const normalizedResponse = normalizeResponse(resp);

            if (handleGlobalApiError(normalizedResponse)) return;

            let authToken = getValue("authToken_BCPW");
            if (!authToken) {
                addToast({ title: "Sitzungsfehler", type: "error" });
                await push("/");
                return;
            }

            clearValue("authToken_BCPW");
            setValue("authToken", authToken);
            await push("/dashboard");
        } finally {
            isFetching = false;
        }
    }
</script>

<ToastStack isMobile={true}/>

<main class="bg-gv-bg-bar w-dvw h-dvh flex items-center justify-center">
    <div class="p-10 h-full w-full bg-white flex flex-col items-center">
        <img
            src={Logo}
            alt="Logo"
            class="min-[530px]:w-1/5 max-[530px]:w-2/5 rounded-full border-2 border-gv-border"
        />
        <p class="font-semibold text-dt-1 mt-5 text-center">Passwort ändern</p>
        <p class="text-gv-light-text text-dt-5 text-center">
            {message}
        </p>

        <Input
            bind:value={currentPw}
            type="password"
            placeholder=""
            title="Aktuelles Passwort"
            marginTop="5"
        />

        <div class="flex flex-col w-full items-start">
            <Input
                bind:value={newPw}
                type="password"
                placeholder=""
                title="Neues Passwort"
                marginTop="5"
            />
            <p class="text-gv-light-text text-dt-6 mt-1">Mindestens 8 Zeichen</p>
        </div>

        <Input
            bind:value={confirmNewPw}
            type="password"
            placeholder=""
            title="Neues Passwort bestätigen"
            marginTop="5"
        />

        <Button
            type="primary"
            marginTop="10"
            onclick={updatePassword}
            isSubmit={true}
            disabled={isFetching}
        >
            <span class="material-symbols-rounded">login</span>
            <p class="ml-3">Passwort ändern</p>
        </Button>
    </div>
</main>